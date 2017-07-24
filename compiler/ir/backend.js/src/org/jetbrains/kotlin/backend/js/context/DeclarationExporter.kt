/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.js.context

import org.jetbrains.kotlin.backend.js.util.buildJs
import org.jetbrains.kotlin.backend.js.util.definePackageAlias
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.isEffectivelyInlineOnly
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.exportedTag
import org.jetbrains.kotlin.js.backend.ast.metadata.staticRef
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.source.getPsi

class DeclarationExporter(
        private val naming: NamingContext,
        private val nameSuggestion: NameSuggestion,
        private val fragment: JsProgramFragment,
        private val config: JsConfig
) {
    private val objectLikeKinds = setOf(ClassKind.OBJECT, ClassKind.ENUM_ENTRY)
    private val exportedDeclarations = mutableSetOf<MemberDescriptor>()
    private val localPackageNames = mutableMapOf<FqName, JsName>()
    private val statements: MutableList<JsStatement>
        get() = fragment.exportBlock.statements

    fun export(descriptor: MemberDescriptor, force: Boolean = false) {
        if (exportedDeclarations.contains(descriptor)) return
        if (descriptor is ConstructorDescriptor && descriptor.isPrimary) return
        if (AnnotationsUtils.isNativeObject(descriptor) || AnnotationsUtils.isLibraryObject(descriptor)) return
        if (descriptor.isEffectivelyInlineOnly()) return

        val suggestedName = nameSuggestion.suggest(descriptor) ?: return

        val container = suggestedName.scope
        if (!descriptor.shouldBeExported(force)) return
        exportedDeclarations += descriptor

        val qualifier = when {
            container is PackageFragmentDescriptor -> {
                getLocalPackageReference(container.fqName)
            }
            DescriptorUtils.isObject(container) -> {
                buildJs { naming.innerNames[container].ref().dotPrototype() }
            }
            else -> {
                buildJs { naming.innerNames[container].ref() }
            }
        }

        when {
            descriptor is ClassDescriptor && descriptor.kind in objectLikeKinds -> {
                exportObject(descriptor, qualifier)
            }
            descriptor is PropertyDescriptor && container is PackageFragmentDescriptor -> {
                exportProperty(descriptor, qualifier)
            }
            else -> {
                assign(descriptor, qualifier)
            }
        }
    }

    private fun assign(descriptor: DeclarationDescriptor, qualifier: JsExpression) {
        val exportedName = naming.innerNames[descriptor]
        val expression = exportedName.makeRef()
        val propertyName = naming.names[descriptor]
        if (propertyName.staticRef == null && exportedName != propertyName) {
            propertyName.staticRef = expression
        }
        statements += buildJs { qualifier.dot(propertyName).assign(expression) }.exportStatement(descriptor)
    }

    private fun exportObject(declaration: ClassDescriptor, qualifier: JsExpression) {
        val name = naming.names[declaration]
        val expression = buildJs { qualifier.defineGetter(name.ident, naming.objectInnerNames[declaration].ref()) }
        statements += expression.exportStatement(declaration)
    }

    private fun exportProperty(declaration: PropertyDescriptor, qualifier: JsExpression) {
        val propertyLiteral = JsObjectLiteral(true)

        val name = naming.names[declaration].ident
        val simpleProperty = JsDescriptorUtils.isSimpleFinalProperty(declaration) &&
                             !JsDescriptorUtils.shouldAccessViaFunctions(declaration)

        val exportedName: JsName
        val getterBody: JsExpression = if (simpleProperty) {
            exportedName = naming.innerNames[declaration]
            val accessToField = JsReturn(exportedName.makeRef())
            JsFunction(fragment.scope, JsBlock(accessToField), "$declaration getter")
        }
        else {
            exportedName = naming.innerNames[declaration.getter!!]
            exportedName.makeRef()
        }
        propertyLiteral.propertyInitializers += JsPropertyInitializer(JsNameRef("get"), getterBody)

        if (declaration.isVar) {
            val setterBody: JsExpression = if (simpleProperty) {
                val statements = mutableListOf<JsStatement>()
                val function = JsFunction(fragment.scope, JsBlock(statements), "$declaration setter")
                function.source = declaration.source.getPsi()
                val valueName = JsScope.declareTemporaryName("value")
                function.parameters += JsParameter(valueName)
                statements += buildJs { statement(naming.innerNames[declaration].ref().assign(valueName.ref())) }
                function
            }
            else {
                naming.innerNames[declaration.setter!!].makeRef()
            }
            propertyLiteral.propertyInitializers += JsPropertyInitializer(JsNameRef("set"), setterBody)
        }

        statements += buildJs { qualifier.defineProperty(name, propertyLiteral) }.exportStatement(declaration)
    }

    private fun getLocalPackageReference(packageName: FqName): JsExpression {
        if (packageName.isRoot) {
            return fragment.scope.declareName("_").makeRef()
        }
        val name = localPackageNames.getOrPut(packageName) {
            JsScope.declareTemporaryName("package$" + packageName.shortName().asString()).also {
                statements += buildJs {
                    val packageRef = getLocalPackageReference(packageName.parent())
                    definePackageAlias(packageName.shortName().asString(), it, packageName.asString(), packageRef)
                }
            }
        }
        return name.makeRef()
    }

    private fun JsExpression.exportStatement(declaration: DeclarationDescriptor) = JsExpressionStatement(this).also {
        it.exportedTag = naming.tags[declaration]
    }

    private fun EffectiveVisibility.publicOrInternal(): Boolean {
        if (publicApi) return true
        if (config.configuration.getBoolean(JSConfigurationKeys.FRIEND_PATHS_DISABLED)) return false
        return toVisibility() == Visibilities.INTERNAL
    }

    private fun MemberDescriptor.shouldBeExported(force: Boolean) =
            force || effectiveVisibility(checkPublishedApi = true).publicOrInternal() || AnnotationsUtils.getJsNameAnnotation(this) != null
}

