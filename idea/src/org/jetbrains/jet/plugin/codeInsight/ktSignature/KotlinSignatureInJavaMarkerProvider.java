/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.codeInsight.ktSignature;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.ide.DataManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.Function;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetIcons;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.plugin.codeInsight.ktSignature.KotlinSignatureUtil.*;

/**
 * @author Evgeny Gerashchenko
 * @since 3 August 2012
 */
public class KotlinSignatureInJavaMarkerProvider implements LineMarkerProvider {
    private static final String SHOW_MARKERS_PROPERTY = "kotlin.signature.markers.enabled";

    private static final Function<PsiElement, String> TOOLTIP_PROVIDER = new Function<PsiElement, String>() {
        @Override
        public String fun(PsiElement element) {
            PsiAnnotation annotation = findKotlinSignatureAnnotation(element);
            assert annotation != null;
            String signature = getKotlinSignature(annotation);
            return "Alternative Kotlin signature is available for this method:\n"
                   + StringUtil.escapeXml(signature);
        }
    };

    private static final GutterIconNavigationHandler<PsiMethod> NAVIGATION_HANDLER = new GutterIconNavigationHandler<PsiMethod>() {
        @Override
        public void navigate(MouseEvent e, PsiMethod element) {
            if (UIUtil.isActionClick(e, MouseEvent.MOUSE_RELEASED)) {
                new EditSignatureAction(element).actionPerformed(DataManager.getInstance().getDataContext(e.getComponent()), e.getPoint());
            }
        }
    };


    @Override
    @Nullable
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        if (isMarkersEnabled(element.getProject()) && findKotlinSignatureAnnotation(element) != null) {
            return new MyLineMarkerInfo(element);
        }
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
    }

    public static boolean isMarkersEnabled(@NotNull Project project) {
        return PropertiesComponent.getInstance(project).getBoolean(SHOW_MARKERS_PROPERTY, true);
    }

    public static void setMarkersEnabled(@NotNull Project project, boolean value) {
        PropertiesComponent.getInstance(project).setValue(SHOW_MARKERS_PROPERTY, Boolean.toString(value));
        refreshMarkers(project);
    }

    private static class MyLineMarkerInfo extends LineMarkerInfo<PsiMethod> {
        public MyLineMarkerInfo(PsiElement element) {
            super((PsiMethod) element, element.getTextOffset(), JetIcons.SMALL_LOGO, Pass.UPDATE_ALL, TOOLTIP_PROVIDER, NAVIGATION_HANDLER);
        }

        @Nullable
        @Override
        public GutterIconRenderer createGutterRenderer() {
            return new LineMarkerGutterIconRenderer<PsiMethod>(this) {
                @Nullable
                @Override
                public ActionGroup getPopupMenuActions() {
                    DefaultActionGroup actionGroup = new DefaultActionGroup();
                    actionGroup.add(new EditSignatureAction(getElement()));
                    return actionGroup;
                }
            };
        }
    }
}
