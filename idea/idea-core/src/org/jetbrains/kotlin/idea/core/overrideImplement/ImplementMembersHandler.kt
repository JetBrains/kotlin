/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.core.overrideImplement

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.util.KotlinIdeaCoreBundle
import org.jetbrains.kotlin.idea.util.expectedDescriptors
import org.jetbrains.kotlin.js.descriptorUtils.hasPrimaryConstructor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.resolve.OverrideResolver

open class ImplementMembersHandler : GenerateMembersHandler(), IntentionAction {
    override fun collectMembersToGenerate(descriptor: ClassDescriptor, project: Project): Collection<OverrideMemberChooserObject> {
        return OverrideResolver.getMissingImplementations(descriptor)
            .map { OverrideMemberChooserObject.create(project, it, it, BodyType.FROM_TEMPLATE) }
    }

    override fun getChooserTitle() = KotlinIdeaCoreBundle.message("implement.members.handler.title")

    override fun getNoMembersFoundHint() = KotlinIdeaCoreBundle.message("implement.members.handler.no.members.hint")

    override fun getText() = familyName
    override fun getFamilyName() = KotlinIdeaCoreBundle.message("implement.members.handler.family")

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile) = isValidFor(editor, file)
}

class ImplementAsConstructorParameter : ImplementMembersHandler() {
    override fun getText() = KotlinIdeaCoreBundle.message("action.text.implement.as.constructor.parameters")

    override fun isValidForClass(classOrObject: KtClassOrObject): Boolean {
        if (classOrObject !is KtClass || classOrObject is KtEnumEntry || classOrObject.isInterface()) return false
        val classDescriptor = classOrObject.resolveToDescriptorIfAny() ?: return false
        if (classDescriptor.isActual) {
            if (classDescriptor.expectedDescriptors().any {
                    it is ClassDescriptor && it.hasPrimaryConstructor()
                }
            ) {
                return false
            }
        }
        return OverrideResolver.getMissingImplementations(classDescriptor).any { it is PropertyDescriptor }
    }

    override fun collectMembersToGenerate(descriptor: ClassDescriptor, project: Project): Collection<OverrideMemberChooserObject> {
        return OverrideResolver.getMissingImplementations(descriptor)
            .filterIsInstance<PropertyDescriptor>()
            .map { OverrideMemberChooserObject.create(project, it, it, BodyType.FROM_TEMPLATE, true) }
    }
}