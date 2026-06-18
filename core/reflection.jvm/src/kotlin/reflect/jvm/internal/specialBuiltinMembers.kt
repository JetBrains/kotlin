/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// This is the descriptor-less counterpart of `specialBuiltinMembers.kt` and `ClassicBuiltinSpecialProperties.kt` in
// `core/descriptors.jvm`. It only ports the logic needed by kotlin-reflect to compute JVM signatures of declarations loaded from builtins
// metadata.

/**
 * If [propertyName] in [container] is a builtin property with a special JVM getter name (e.g. `name` for `kotlin.Enum.name`, `keySet` for
 * `kotlin.collections.Map.keys`), returns that name. Returns `null` for ordinary properties.
 */
internal fun getBuiltinSpecialPropertyGetterName(propertyName: String, container: KDeclarationContainerImpl): String? {
    if (Name.identifier(propertyName) !in BuiltinSpecialProperties.SPECIAL_SHORT_NAMES) return null
    val fqName = container.findBuiltinSpecialPropertyFqName(propertyName) ?: return null
    return BuiltinSpecialProperties.PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP[fqName]?.asString()
}

private fun KDeclarationContainerImpl.findBuiltinSpecialPropertyFqName(propertyName: String): FqName? {
    val klass = this as? KClassImpl<*> ?: return null
    val fqName = klass.classId.asSingleFqName().child(Name.identifier(propertyName))
    if (fqName in BuiltinSpecialProperties.SPECIAL_FQ_NAMES) return fqName
    for (supertype in klass.supertypes) {
        (supertype.classifier as? KClassImpl<*>)?.findBuiltinSpecialPropertyFqName(propertyName)?.let { return it }
    }
    return null
}
