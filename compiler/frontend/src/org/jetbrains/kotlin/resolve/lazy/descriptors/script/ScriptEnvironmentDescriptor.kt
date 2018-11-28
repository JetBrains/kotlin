/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.lazy.descriptors.script

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.MutableClassDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyScriptDescriptor
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.Printer

class ScriptEnvironmentDescriptor(script: LazyScriptDescriptor) :
    MutableClassDescriptor(
        script,
        ClassKind.CLASS, false, false,
        Name.special("<synthetic script environment for ${script.name}>"),
        SourceElement.NO_SOURCE,
        LockBasedStorageManager.NO_LOCKS
    ) {

    init {
        modality = Modality.FINAL
        visibility = Visibilities.PUBLIC
        setTypeParameterDescriptors(emptyList())
        createTypeConstructor()
    }

    private val memberScope: () -> ScriptEnvironmentMemberScope = script.resolveSession.storageManager.createLazyValue {
        ScriptEnvironmentMemberScope(
            script.name.identifier,
            properties()
        )
    }

    override fun getUnsubstitutedMemberScope(): MemberScope = memberScope()

    val properties: () -> List<ScriptEnvironmentPropertyDescriptor> = script.resolveSession.storageManager.createLazyValue {
        script.scriptDefinition().environmentVariables.mapNotNull { (name, type) ->
            script.findTypeDescriptor(type, Errors.MISSING_SCRIPT_ENVIRONMENT_PROPERTY_CLASS)?.let {
                name to it
            }
        }.map { (name, classDescriptor) ->
            ScriptEnvironmentPropertyDescriptor(
                Name.identifier(name),
                classDescriptor,
                thisAsReceiverParameter,
                true,
                script
            )
        }
    }

    private class ScriptEnvironmentMemberScope(
        private val scriptId: String,
        private val environmentProperties: List<PropertyDescriptor>
    ) : MemberScopeImpl() {
        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> =
            environmentProperties

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
            environmentProperties.filter { it.name == name }

        override fun printScopeStructure(p: Printer) {
            p.println("Scope of script environment: $scriptId")
        }
    }
}