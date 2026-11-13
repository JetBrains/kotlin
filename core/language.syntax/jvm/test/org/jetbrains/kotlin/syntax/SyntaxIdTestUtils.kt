/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.assertTrue

internal class SyntaxIdProperty(val name: String, private val property: KProperty1<out Any, *>) {
    val isConst: Boolean
        get() = property.isConst

    val value: SyntaxId
        get() = property.getter.call() as SyntaxId
}

/**
 * All syntax ID properties declared in the ID holder objects (excluding [NO_ID]), in declaration order of the holders.
 */
internal fun collectSyntaxIdProperties(): List<SyntaxIdProperty> = buildList {
    for (holder in listOf(
        BasicSyntaxElementTypeIds::class,
        KotlinSyntaxTokenIds::class,
        KotlinSyntaxNodeTypeIds::class,
        KDocSyntaxTokenIds::class,
        KDocSyntaxNodeTypeIds::class,
    )) {
        val properties = holder.declaredMemberProperties
        assertTrue(properties.isNotEmpty(), "No IDs found in ${holder.simpleName}")
        for (property in properties) {
            add(SyntaxIdProperty("${holder.simpleName}.${property.name}", property))
        }
    }
}
