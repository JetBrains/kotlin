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
package org.jetbrains.kotlin.idea.debugger.breakpoints;

import com.intellij.debugger.InstanceFilter;
import com.intellij.debugger.ui.breakpoints.EditClassFiltersDialog;
import com.intellij.debugger.ui.breakpoints.EditInstanceFiltersDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.ui.FieldPanel;
import com.intellij.ui.MultiLineTooltipUI;
import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase;
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.debugger.breakpoints.properties.JavaBreakpointProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class KotlinBreakpointFiltersPanel<T extends KotlinPropertyBreakpointProperties, B extends XBreakpoint<T>> extends XBreakpointCustomPropertiesPanel<B> {
  private JPanel myConditionsPanel;
  private JPanel myInstanceFiltersPanel;
  private JCheckBox myInstanceFiltersCheckBox;
  private JPanel myInstanceFiltersFieldPanel;
  private JPanel myClassFiltersPanel;
  private JCheckBox myClassFiltersCheckBox;
  private JPanel myClassFiltersFieldPanel;
  private JPanel myPassCountPanel;
  private JCheckBox myPassCountCheckbox;
  private JTextField myPassCountField;

  private final FieldPanel myInstanceFiltersField;
  private final FieldPanel myClassFiltersField;

  private ClassFilter[] myClassFilters = ClassFilter.EMPTY_ARRAY;
  private ClassFilter[] myClassExclusionFilters = ClassFilter.EMPTY_ARRAY;
  private InstanceFilter[] myInstanceFilters = InstanceFilter.EMPTY_ARRAY;
  protected final Project myProject;

  private PsiClass myBreakpointPsiClass;

  public KotlinBreakpointFiltersPanel(Project project) {
    myProject = project;
    myInstanceFiltersField = new FieldPanel(new MyTextField(), "", null,
                                            new ActionListener() {
                                              @Override
                                              public void actionPerformed(ActionEvent e) {
                                                reloadInstanceFilters();
                                                EditInstanceFiltersDialog _dialog = new EditInstanceFiltersDialog(myProject);
                                                _dialog.setFilters(myInstanceFilters);
                                                _dialog.show();
                                                if (_dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
                                                  myInstanceFilters = _dialog.getFilters();
                                                  updateInstanceFilterEditor(true);
                                                }
                                              }
                                            },
                                            null
    );

    myClassFiltersField = new FieldPanel(new MyTextField(), "", null,
                                         new ActionListener() {
                                           @Override
                                           public void actionPerformed(ActionEvent e) {
                                             reloadClassFilters();

                                             com.intellij.ide.util.ClassFilter classFilter = createClassConditionFilter();

                                             EditClassFiltersDialog _dialog = new EditClassFiltersDialog(myProject, classFilter);
                                             _dialog.setFilters(myClassFilters, myClassExclusionFilters);
                                             _dialog.show();
                                             if (_dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
                                               myClassFilters = _dialog.getFilters();
                                               myClassExclusionFilters = _dialog.getExclusionFilters();
                                               updateClassFilterEditor(true);
                                             }
                                           }
                                         },
                                         null
    );

    ActionListener updateListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateCheckboxes();
      }
    };

    myPassCountCheckbox.addActionListener(updateListener);
    myInstanceFiltersCheckBox.addActionListener(updateListener);
    myClassFiltersCheckBox.addActionListener(updateListener);

    ToolTipManager.sharedInstance().registerComponent(myClassFiltersField.getTextField());
    ToolTipManager.sharedInstance().registerComponent(myInstanceFiltersField.getTextField());

    insert(myInstanceFiltersFieldPanel, myInstanceFiltersField);
    insert(myClassFiltersFieldPanel, myClassFiltersField);

    DebuggerUIUtil.focusEditorOnCheck(myPassCountCheckbox, myPassCountField);
    DebuggerUIUtil.focusEditorOnCheck(myInstanceFiltersCheckBox, myInstanceFiltersField.getTextField());
    DebuggerUIUtil.focusEditorOnCheck(myClassFiltersCheckBox, myClassFiltersField.getTextField());
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myConditionsPanel;
  }

  @Override
  public boolean isVisibleOnPopup(@NotNull B breakpoint) {
    JavaBreakpointProperties properties = breakpoint.getProperties();
    if (properties != null) {
      return properties.isCOUNT_FILTER_ENABLED() || properties.isCLASS_FILTERS_ENABLED() || properties.isINSTANCE_FILTERS_ENABLED();
    }
    return false;
  }

  @Override
  public void saveTo(@NotNull B breakpoint) {
    JavaBreakpointProperties properties = breakpoint.getProperties();
    if (properties == null) {
      return;
    }

    boolean changed = false;
    try {
      String text = myPassCountField.getText().trim();
      int filter = !text.isEmpty() ? Integer.parseInt(text) : 0;
      if (filter < 0) filter = 0;
      changed = properties.setCOUNT_FILTER(filter);
    }
    catch (Exception ignored) {
    }

    changed = properties.setCOUNT_FILTER_ENABLED(properties.getCOUNT_FILTER() > 0 && myPassCountCheckbox.isSelected()) || changed;
    reloadInstanceFilters();
    reloadClassFilters();
    updateInstanceFilterEditor(true);
    updateClassFilterEditor(true);

    changed = properties.setINSTANCE_FILTERS_ENABLED(myInstanceFiltersField.getText().length() > 0 && myInstanceFiltersCheckBox.isSelected()) || changed;
    changed = properties.setCLASS_FILTERS_ENABLED(myClassFiltersField.getText().length() > 0 && myClassFiltersCheckBox.isSelected()) || changed;
    changed = properties.setClassFilters(myClassFilters) || changed;
    changed = properties.setClassExclusionFilters(myClassExclusionFilters) || changed;
    changed = properties.setInstanceFilters(myInstanceFilters) || changed;
    if (changed) {
      ((XBreakpointBase)breakpoint).fireBreakpointChanged();
    }
  }

  private static void insert(JPanel panel, JComponent component) {
    panel.setLayout(new BorderLayout());
    panel.add(component, BorderLayout.CENTER);
  }

  @Override
  public void loadFrom(@NotNull B breakpoint) {
    JavaBreakpointProperties properties = breakpoint.getProperties();
    if (properties != null) {
      if (properties.getCOUNT_FILTER() > 0) {
        myPassCountField.setText(Integer.toString(properties.getCOUNT_FILTER()));
      }
      else {
        myPassCountField.setText("");
      }

      myPassCountCheckbox.setSelected(properties.isCOUNT_FILTER_ENABLED());

      myInstanceFiltersCheckBox.setSelected(properties.isINSTANCE_FILTERS_ENABLED());
      myInstanceFiltersField.setEnabled(properties.isINSTANCE_FILTERS_ENABLED());
      myInstanceFiltersField.getTextField().setEditable(properties.isINSTANCE_FILTERS_ENABLED());
      myInstanceFilters = properties.getInstanceFilters();
      updateInstanceFilterEditor(true);

      myClassFiltersCheckBox.setSelected(properties.isCLASS_FILTERS_ENABLED());
      myClassFiltersField.setEnabled(properties.isCLASS_FILTERS_ENABLED());
      myClassFiltersField.getTextField().setEditable(properties.isCLASS_FILTERS_ENABLED());
      myClassFilters = properties.getClassFilters();
      myClassExclusionFilters = properties.getClassExclusionFilters();
      updateClassFilterEditor(true);

      XSourcePosition position = breakpoint.getSourcePosition();
      // TODO: need to calculate psi class
      //myBreakpointPsiClass = breakpoint.getPsiClass();
    }
    updateCheckboxes();
  }

  private void updateInstanceFilterEditor(boolean updateText) {
    List<String> filters = new ArrayList<String>();
    for (InstanceFilter instanceFilter : myInstanceFilters) {
      if (instanceFilter.isEnabled()) {
        filters.add(Long.toString(instanceFilter.getId()));
      }
    }
    if (updateText) {
      myInstanceFiltersField.setText(StringUtil.join(filters, " "));
    }

    String tipText = concatWithEx(filters, " ", (int)Math.sqrt(myInstanceFilters.length) + 1, "\n");
    myInstanceFiltersField.getTextField().setToolTipText(tipText);
  }

  private class MyTextField extends JTextField {
    public MyTextField() {
    }

    @Override
    public String getToolTipText(MouseEvent event) {
      reloadClassFilters();
      updateClassFilterEditor(false);
      reloadInstanceFilters();
      updateInstanceFilterEditor(false);
      String toolTipText = super.getToolTipText(event);
      return getToolTipText().length() == 0 ? null : toolTipText;
    }

    @Override
    public JToolTip createToolTip() {
      JToolTip toolTip = new JToolTip(){{
        setUI(new MultiLineTooltipUI());
      }};
      toolTip.setComponent(this);
      return toolTip;
    }
  }

  private void reloadClassFilters() {
    String filtersText = myClassFiltersField.getText();

    ArrayList<ClassFilter> classFilters     = new ArrayList<ClassFilter>();
    ArrayList<ClassFilter> exclusionFilters = new ArrayList<ClassFilter>();
    int startFilter = -1;
    for(int i = 0; i <= filtersText.length(); i++) {
      if(i < filtersText.length() && !Character.isWhitespace(filtersText.charAt(i))){
        if(startFilter == -1) {
          startFilter = i;
        }
      }
      else {
        if(startFilter >=0) {
          if(filtersText.charAt(startFilter) == '-') {
            exclusionFilters.add(new ClassFilter(filtersText.substring(startFilter + 1, i)));
          }
          else {
            classFilters.add(new ClassFilter(filtersText.substring(startFilter, i)));
          }
          startFilter = -1;
        }
      }
    }
    for (ClassFilter classFilter : myClassFilters) {
      if (!classFilter.isEnabled()) {
        classFilters.add(classFilter);
      }
    }
    for (ClassFilter classFilter : myClassExclusionFilters) {
      if (!classFilter.isEnabled()) {
        exclusionFilters.add(classFilter);
      }
    }
    myClassFilters          = classFilters    .toArray(new ClassFilter[classFilters    .size()]);
    myClassExclusionFilters = exclusionFilters.toArray(new ClassFilter[exclusionFilters.size()]);
  }

  private void reloadInstanceFilters() {
    String filtersText = myInstanceFiltersField.getText();

    ArrayList<InstanceFilter> idxs = new ArrayList<InstanceFilter>();
    int startNumber = -1;
    for(int i = 0; i <= filtersText.length(); i++) {
      if(i < filtersText.length() && Character.isDigit(filtersText.charAt(i))) {
        if(startNumber == -1) {
          startNumber = i;
        }
      }
      else {
        if(startNumber >=0) {
          idxs.add(InstanceFilter.create(filtersText.substring(startNumber, i)));
          startNumber = -1;
        }
      }
    }
    for (InstanceFilter instanceFilter : myInstanceFilters) {
      if (!instanceFilter.isEnabled()) {
        idxs.add(instanceFilter);
      }
    }
    myInstanceFilters = idxs.toArray(new InstanceFilter[idxs.size()]);
  }

  private void updateClassFilterEditor(boolean updateText) {
    List<String> filters = new ArrayList<String>();
    for (ClassFilter classFilter : myClassFilters) {
      if (classFilter.isEnabled()) {
        filters.add(classFilter.getPattern());
      }
    }
    List<String> excludeFilters = new ArrayList<String>();
    for (ClassFilter classFilter : myClassExclusionFilters) {
      if (classFilter.isEnabled()) {
        excludeFilters.add("-" + classFilter.getPattern());
      }
    }
    if (updateText) {
      String editorText = StringUtil.join(filters, " ");
      if(!filters.isEmpty()) {
        editorText += " ";
      }
      editorText += StringUtil.join(excludeFilters, " ");
      myClassFiltersField.setText(editorText);
    }

    int width = (int)Math.sqrt(myClassExclusionFilters.length + myClassFilters.length) + 1;
    String tipText = concatWithEx(filters, " ", width, "\n");
    if(!filters.isEmpty()) {
      tipText += "\n";
    }
    tipText += concatWithEx(excludeFilters, " ", width, "\n");
    myClassFiltersField.getTextField().setToolTipText(tipText);
  }

  private static String concatWithEx(List<String> s, String concator, int N, String NthConcator) {
    String result = "";
    int i = 1;
    for (Iterator iterator = s.iterator(); iterator.hasNext(); i++) {
      String str = (String) iterator.next();
      result += str;
      if(iterator.hasNext()){
        if(i % N == 0){
          result += NthConcator;
        }
        else {
          result += concator;
        }
      }
    }
    return result;
  }

  protected com.intellij.ide.util.ClassFilter createClassConditionFilter() {
    com.intellij.ide.util.ClassFilter classFilter;
    if(myBreakpointPsiClass != null) {
      classFilter = new com.intellij.ide.util.ClassFilter() {
        @Override
        public boolean isAccepted(PsiClass aClass) {
          return myBreakpointPsiClass == aClass || aClass.isInheritor(myBreakpointPsiClass, true);
        }
      };
    }
    else {
      classFilter = null;
    }
    return classFilter;
  }

  protected void updateCheckboxes() {
    boolean passCountApplicable = true;
    if (myInstanceFiltersCheckBox.isSelected() || myClassFiltersCheckBox.isSelected()) {
      passCountApplicable = false;
    }
    myPassCountCheckbox.setEnabled(passCountApplicable);

    boolean passCountSelected = myPassCountCheckbox.isSelected();
    myInstanceFiltersCheckBox.setEnabled(!passCountSelected);
    myClassFiltersCheckBox.setEnabled(!passCountSelected);

    myPassCountField.setEditable(myPassCountCheckbox.isSelected());
    myPassCountField.setEnabled (myPassCountCheckbox.isSelected());

    myInstanceFiltersField.setEnabled(myInstanceFiltersCheckBox.isSelected());
    myInstanceFiltersField.getTextField().setEditable(myInstanceFiltersCheckBox.isSelected());

    myClassFiltersField.setEnabled(myClassFiltersCheckBox.isSelected());
    myClassFiltersField.getTextField().setEditable(myClassFiltersCheckBox.isSelected());
  }
}
