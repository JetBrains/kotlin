/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Member
import java.lang.reflect.Modifier
import kotlin.metadata.Modality
import kotlin.reflect.KVisibility

internal abstract class JavaKCallable<out R>(
    override val container: KDeclarationContainerImpl,
    val member: Member,
    override val rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
) : ReflectKCallableImpl<R>(overriddenStorage) {
    override val visibility: KVisibility?
        get() = member.modifiers.computeVisibilityForJavaModifiers()

    final override val modality: Modality
        get() = overriddenStorage.modality ?: when {
            Modifier.isFinal(member.modifiers) -> Modality.FINAL
            Modifier.isAbstract(member.modifiers) -> Modality.ABSTRACT
            else -> Modality.OPEN
        }

    final override val isSuspend: Boolean
        get() = false

    final override val isPackagePrivate: Boolean
        get() = member.modifiers.isPackagePrivate
}
