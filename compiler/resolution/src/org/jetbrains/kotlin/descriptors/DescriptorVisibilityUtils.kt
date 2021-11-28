/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.*
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue


object DescriptorVisibilityUtils {
    @JvmStatic
    fun findInvisibleMember(
        receiver: ReceiverValue?,
        what: DeclarationDescriptorWithVisibility,
        from: DeclarationDescriptor,
        languageVersionSettings: LanguageVersionSettings
    ): DeclarationDescriptorWithVisibility? {
        return findInvisibleMember(receiver, what, from, languageVersionSettings.useSpecialRulesForPrivateSealedConstructors)
    }

    @JvmStatic
    fun isVisible(
        receiver: ReceiverValue?,
        what: DeclarationDescriptorWithVisibility,
        from: DeclarationDescriptor,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        return isVisible(receiver, what, from, languageVersionSettings.useSpecialRulesForPrivateSealedConstructors)
    }

    @JvmStatic
    fun isVisibleIgnoringReceiver(
        what: DeclarationDescriptorWithVisibility,
        from: DeclarationDescriptor,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        return isVisibleIgnoringReceiver(what, from, languageVersionSettings.useSpecialRulesForPrivateSealedConstructors)
    }

    @JvmStatic
    fun isVisibleWithAnyReceiver(
        what: DeclarationDescriptorWithVisibility,
        from: DeclarationDescriptor,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        return isVisibleWithAnyReceiver(what, from, languageVersionSettings.useSpecialRulesForPrivateSealedConstructors)
    }

    val LanguageVersionSettings.useSpecialRulesForPrivateSealedConstructors: Boolean
        get() = !supportsFeature(LanguageFeature.AllowSealedInheritorsInDifferentFilesOfSamePackage) ||
                !supportsFeature(LanguageFeature.UseConsistentRulesForPrivateConstructorsOfSealedClasses)
}
