/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.symbols.BirTypedSymbol
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality

fun <S : BirTypedSymbol<E>, E : BirOverridableDeclaration<S>> E.collectRealOverrides(
    toSkip: (E) -> Boolean = { false },
    filter: (E) -> Boolean = { false }
): Set<E> {
    if (!isFakeOverride && !toSkip(this)) return setOf(this)

    @Suppress("UNCHECKED_CAST")
    return this.overriddenSymbols
        .map { it as E }
        .collectAndFilterRealOverrides(toSkip, filter)
}

fun <S : BirTypedSymbol<E>, E : BirOverridableDeclaration<S>> Collection<E>.collectAndFilterRealOverrides(
    toSkip: (E) -> Boolean = { false },
    filter: (E) -> Boolean = { false }
): Set<E> {

    val visited = mutableSetOf<E>()
    val realOverrides = mutableMapOf<Any, E>()

    /*
        Due to IR copying in performByBirFile, overrides should only be distinguished up to their signatures.
     */
    fun E.toKey(): Any = signature ?: this

    fun collectRealOverrides(member: E) {
        if (!visited.add(member) || filter(member)) return

        if (!member.isFakeOverride && !toSkip(member)) {
            realOverrides[member.toKey()] = member
        } else {
            @Suppress("UNCHECKED_CAST")
            member.overriddenSymbols.forEach { collectRealOverrides(it as E) }
        }
    }

    this.forEach { collectRealOverrides(it) }

    fun excludeRepeated(member: E) {
        if (!visited.add(member)) return

        member.overriddenSymbols.forEach {
            @Suppress("UNCHECKED_CAST")
            val owner = it as E
            realOverrides.remove(owner.toKey())
            excludeRepeated(owner)
        }
    }

    visited.clear()
    realOverrides.toList().forEach { excludeRepeated(it.second) }

    return realOverrides.values.toSet()
}

@Suppress("UNCHECKED_CAST")
fun Collection<BirOverridableMember>.collectAndFilterRealOverrides(): Set<BirOverridableMember> = when {
    all { it is BirSimpleFunction } -> (this as Collection<BirSimpleFunction>).collectAndFilterRealOverrides()
    all { it is BirProperty } -> (this as Collection<BirProperty>).collectAndFilterRealOverrides()
    else -> error("all members should be of the same kind, got ${map { it.render() }}")
}

fun <S : BirTypedSymbol<E>, E : BirOverridableDeclaration<S>> E.resolveFakeOverride(
    allowAbstract: Boolean = false,
    toSkip: (E) -> Boolean = { false }
): E? =
    resolveFakeOverrideOrNull(allowAbstract, toSkip).also {
        if (allowAbstract && it == null) {
            error("No real overrides for ${this.render()}")
        }
    }

// TODO: use this implementation instead of any other
fun <S : BirTypedSymbol<E>, E : BirOverridableDeclaration<S>> E.resolveFakeOverrideOrNull(
    allowAbstract: Boolean = false,
    toSkip: (E) -> Boolean = { false }
): E? {
    if (!isFakeOverride && !toSkip(this)) return this
    return if (allowAbstract) {
        collectRealOverrides(toSkip).firstOrNull()
    } else {
        collectRealOverrides(toSkip, { it.modality == Modality.ABSTRACT })
            .let { realOverrides ->
                // Kotlin forbids conflicts between overrides, but they may trickle down from Java.
                realOverrides.singleOrNull { (it.parent as? BirClass)?.kind != ClassKind.INTERFACE }
                // TODO: We take firstOrNull instead of singleOrNull here because of KT-36188.
                    ?: realOverrides.firstOrNull()
            }
    }
}
