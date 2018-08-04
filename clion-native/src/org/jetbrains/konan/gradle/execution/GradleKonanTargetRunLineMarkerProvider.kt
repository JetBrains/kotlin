/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.NotNullLazyKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.jetbrains.cidr.execution.CidrTargetRunConfigurationProducer
import com.jetbrains.cidr.execution.CidrTargetRunLineMarkerProvider

class GradleKonanTargetRunLineMarkerProvider : CidrTargetRunLineMarkerProvider() {

  override fun getInfo(e: PsiElement): RunLineMarkerContributor.Info? {
    return if (ourHasExeTarget.getValue(e.containingFile).value) {
      getInfo(e, true)
    }
    else null
  }

  companion object {
    private val ourHasExeTarget = NotNullLazyKey.create<CachedValue<Boolean>, PsiFile>(
      "HasExeTarget") { file ->
      CachedValuesManager.getManager(file.project).createCachedValue(
        {
          val instance = GradleKonanTargetRunConfigurationProducer.getGradleKonanInstance(file.project)
          CachedValueProvider.Result(
            instance != null && !instance.getExecutableTargetsForFile(file).isEmpty(),
            ModificationTracker.NEVER_CHANGED)
        },
        false)
    }
  }
}