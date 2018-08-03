/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.migration

import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.codeInspection.ex.InspectionToolWrapper
import com.intellij.codeInspection.ex.createSimple
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.profile.codeInspection.InspectionProfileManager
import com.intellij.psi.PsiElement
import org.jdom.Element
import org.jetbrains.kotlin.idea.quickfix.migration.MigrationFix
import java.util.LinkedHashSet

fun createMigrationProfile(managerEx: InspectionManagerEx, psiElement: PsiElement?): InspectionProfileImpl {
    val rootProfile = InspectionProfileManager.getInstance().currentProfile

    val migrationFixWrappers = rootProfile.allTools.asSequence()
        .map { it.tool }
        .filter { toolWrapper: InspectionToolWrapper<*, *> ->
            toolWrapper.tool is MigrationFix
        }
        .toList()

    val allWrappers = LinkedHashSet<InspectionToolWrapper<*, *>>()
    for (toolWrapper in migrationFixWrappers) {
        allWrappers.add(toolWrapper)
        rootProfile.collectDependentInspections(toolWrapper, allWrappers, managerEx.project)
    }

    val model = createSimple("Migration", managerEx.project, migrationFixWrappers)
    try {
        val element = Element("toCopy")
        for (wrapper in migrationFixWrappers) {
            wrapper.tool.writeSettings(element)
            val tw = (if (psiElement == null)
                model.getInspectionTool(wrapper.shortName, managerEx.project)
            else
                model.getInspectionTool(wrapper.shortName, psiElement))!!

            tw.tool.readSettings(element)
        }
    } catch (ignored: WriteExternalException) {
    } catch (ignored: InvalidDataException) {
    }

    return model
}