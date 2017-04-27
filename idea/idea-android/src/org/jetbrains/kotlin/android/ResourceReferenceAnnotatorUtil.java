/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android;


import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.resources.ResourceItem;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.resources.ResourceResolver;
import com.android.resources.ResourceType;
import com.android.tools.idea.configurations.Configuration;
import com.android.tools.idea.configurations.ConfigurationManager;
import com.android.tools.idea.res.AppResourceRepository;
import com.android.tools.idea.res.ResourceHelper;
import com.android.tools.idea.ui.resourcechooser.ColorPicker;
import com.android.utils.XmlUtils;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ui.ColorIcon;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import static com.android.SdkConstants.*;
import static com.android.tools.idea.uibuilder.property.renderer.NlDefaultRenderer.ICON_SIZE;

/**
 *  Contains copied privates from AndroidColorAnnotator, so we could use them for Kotlin AndroidResourceReferenceAnnotator
 */
public class ResourceReferenceAnnotatorUtil {
    @Nullable
    public static File pickBitmapFromXml(@NotNull File file, @NotNull ResourceResolver resourceResolver, @NotNull Project project) {
        try {
            String xml = Files.toString(file, Charsets.UTF_8);
            Document document = XmlUtils.parseDocumentSilently(xml, true);
            if (document != null && document.getDocumentElement() != null) {
                Element root = document.getDocumentElement();
                String tag = root.getTagName();
                Element target = null;
                String attribute = null;
                if ("vector".equals(tag)) {
                    // Vectors are handled in the icon cache
                    return file;
                }
                else if ("bitmap".equals(tag) || "nine-patch".equals(tag)) {
                    target = root;
                    attribute = ATTR_SRC;
                }
                else if ("selector".equals(tag) ||
                         "level-list".equals(tag) ||
                         "layer-list".equals(tag) ||
                         "transition".equals(tag)) {
                    NodeList children = root.getChildNodes();
                    for (int i = children.getLength() - 1; i >= 0; i--) {
                        Node item = children.item(i);
                        if (item.getNodeType() == Node.ELEMENT_NODE && TAG_ITEM.equals(item.getNodeName())) {
                            target = (Element)item;
                            if (target.hasAttributeNS(ANDROID_URI, ATTR_DRAWABLE)) {
                                attribute = ATTR_DRAWABLE;
                                break;
                            }
                        }
                    }
                }
                else if ("clip".equals(tag) || "inset".equals(tag) || "scale".equals(tag)) {
                    target = root;
                    attribute = ATTR_DRAWABLE;
                } else {
                    // <shape> etc - no bitmap to be found
                    return null;
                }
                if (attribute != null && target.hasAttributeNS(ANDROID_URI, attribute)) {
                    String src = target.getAttributeNS(ANDROID_URI, attribute);
                    ResourceValue value = resourceResolver.findResValue(src, false);
                    if (value != null) {
                        return ResourceHelper.resolveDrawable(resourceResolver, value, project);

                    }
                }
            }
        } catch (Throwable ignore) {
            // Not logging for now; afraid to risk unexpected crashes in upcoming preview. TODO: Re-enable.
            //Logger.getInstance(AndroidColorAnnotator.class).warn(String.format("Could not read/render icon image %1$s", file), e);
        }

        return null;
    }

    /** Looks up the resource item of the given type and name for the given configuration, if any */
    @Nullable
    public static ResourceValue findResourceValue(ResourceType type,
            String name,
            boolean isFramework,
            Module module,
            Configuration configuration) {
        if (isFramework) {
            ResourceRepository frameworkResources = configuration.getFrameworkResources();
            if (frameworkResources == null) {
                return null;
            }
            if (!frameworkResources.hasResourceItem(type, name)) {
                return null;
            }
            ResourceItem item = frameworkResources.getResourceItem(type, name);
            return item.getResourceValue(type, configuration.getFullConfig(), false);
        } else {
            AppResourceRepository appResources = AppResourceRepository.getOrCreateInstance(module);
            if (appResources == null) {
                return null;
            }
            if (!appResources.hasResourceItem(type, name)) {
                return null;
            }
            return appResources.getConfiguredValue(type, name, configuration.getFullConfig());
        }
    }

    /** Picks a suitable configuration to use for resource resolution */
    @Nullable
    public static Configuration pickConfiguration(AndroidFacet facet, Module module, PsiFile file) {
        VirtualFile virtualFile = file.getVirtualFile();
        if(virtualFile == null) {
            return null;
        } else {
            VirtualFile parent = virtualFile.getParent();
            if(parent == null) {
                return null;
            } else {
                String parentName = parent.getName();
                VirtualFile layout;
                if(!parentName.startsWith("layout")) {
                    layout = ResourceHelper.pickAnyLayoutFile(module, facet);
                    if(layout == null) {
                        return null;
                    }
                } else {
                    layout = virtualFile;
                }

                return ConfigurationManager.getOrCreateInstance(module).getConfiguration(layout);
            }
        }
    }

    public static class ColorRenderer extends GutterIconRenderer {
        private final PsiElement myElement;
        private final Color myColor;

        ColorRenderer(@NotNull PsiElement element, @Nullable Color color) {
            myElement = element;
            myColor = color;
        }

        @NotNull
        @Override
        public Icon getIcon() {
            Color color = getCurrentColor();
            return JBUI.scale(color == null ? EmptyIcon.create(ICON_SIZE) : new ColorIcon(ICON_SIZE, color));
        }

        @Nullable
        private Color getCurrentColor() {
            if (myColor != null) {
                return myColor;
            } else if (myElement instanceof XmlTag) {
                return ResourceHelper.parseColor(((XmlTag)myElement).getValue().getText());
            } else if (myElement instanceof XmlAttributeValue) {
                return ResourceHelper.parseColor(((XmlAttributeValue)myElement).getValue());
            } else {
                return null;
            }
        }

        @Override
        public AnAction getClickAction() {
            if (myColor != null) { // Cannot set colors that were derived
                return null;
            }
            return new AnAction() {
                @Override
                public void actionPerformed(AnActionEvent e) {
                    final Editor editor = CommonDataKeys.EDITOR.getData(e.getDataContext());
                    if (editor != null) {
                        // Need ARGB support in platform color chooser; see
                        //  https://youtrack.jetbrains.com/issue/IDEA-123498
                        //final Color color =
                        //  ColorChooser.chooseColor(editor.getComponent(), AndroidBundle.message("android.choose.color"), getCurrentColor());
                        final Color color = ColorPicker.showDialog(editor.getComponent(), "Choose Color", getCurrentColor(), true, null, false);
                        if (color != null) {
                            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                @Override
                                public void run() {
                                    if (myElement instanceof XmlTag) {
                                        ((XmlTag)myElement).getValue().setText(ResourceHelper.colorToString(color));
                                    } else if (myElement instanceof XmlAttributeValue) {
                                        XmlAttribute attribute = PsiTreeUtil.getParentOfType(myElement, XmlAttribute.class);
                                        if (attribute != null) {
                                            attribute.setValue(ResourceHelper.colorToString(color));
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            };
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ColorRenderer that = (ColorRenderer)o;
            // TODO: Compare with modification count in app resources (if not framework)
            if (myColor != null ? !myColor.equals(that.myColor) : that.myColor != null) return false;
            if (!myElement.equals(that.myElement)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = myElement.hashCode();
            result = 31 * result + (myColor != null ? myColor.hashCode() : 0);
            return result;
        }
    }
}
