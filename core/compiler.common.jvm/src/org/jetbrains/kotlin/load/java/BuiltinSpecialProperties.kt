/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name

object BuiltinSpecialProperties {
    val PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP: Map<FqName, Name> = mapOf(
        StandardNames.FqNames._enum.childSafe("name") to StandardNames.NAME,
        StandardNames.FqNames._enum.childSafe("ordinal") to Name.identifier("ordinal"),
        StandardNames.FqNames.collection.child("size") to Name.identifier("size"),
        StandardNames.FqNames.map.child("size") to Name.identifier("size"),
        StandardNames.FqNames.charSequence.childSafe("length") to Name.identifier("length"),
        StandardNames.FqNames.map.child("keys") to Name.identifier("keySet"),
        StandardNames.FqNames.map.child("values") to Name.identifier("values"),
        StandardNames.FqNames.map.child("entries") to Name.identifier("entrySet")
    )

    private val GETTER_JVM_NAME_TO_PROPERTIES_SHORT_NAME_MAP: Map<Name, List<Name>> =
        PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP.entries
            .map { Pair(it.key.shortName(), it.value) }
            .groupBy({ it.second }, { it.first })
            .mapValues {
                it.value.distinct()
            }

    val GETTER_FQ_NAMES: Set<FqName> = PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP.mapTo(mutableSetOf()) {
        JavaToKotlinClassMap.mapKotlinToJava(it.key.parent().toUnsafe())!!.asSingleFqName().child(it.value)
    }

    val SPECIAL_FQ_NAMES: Set<FqName> = PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP.keys
    val SPECIAL_SHORT_NAMES: Set<Name> = SPECIAL_FQ_NAMES.map(FqName::shortName).toSet()

    fun getPropertyNameCandidatesBySpecialGetterName(name1: Name): List<Name> =
        GETTER_JVM_NAME_TO_PROPERTIES_SHORT_NAME_MAP[name1] ?: emptyList()
}

private fun FqName.child(name: String): FqName = child(Name.identifier(name))
private fun FqNameUnsafe.childSafe(name: String): FqName = child(Name.identifier(name)).toSafe()
