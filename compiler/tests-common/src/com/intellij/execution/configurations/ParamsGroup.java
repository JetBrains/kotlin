/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.intellij.execution.configurations;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ParamsGroup implements Cloneable {

    private final String myGroupId;
    private final ParametersList myParamList;

    public ParamsGroup(@NotNull String groupId) {
        this(groupId, new ParametersList());
    }

    private ParamsGroup(@NotNull String groupId, @NotNull ParametersList paramList) {
        myGroupId = groupId;
        myParamList = paramList;
    }

    @NotNull
    public String getId() {
        return myGroupId;
    }

    public void addParameter(@NotNull String parameter) {
        myParamList.add(parameter);
    }

    public void addParameterAt(int index, @NotNull String parameter) {
        myParamList.addAt(index, parameter);
    }

    public void addParameters(@NotNull String ... parameters) {
        for (String parameter : parameters) {
            addParameter(parameter);
        }
    }

    public void addParameters(@NotNull List<String> parameters) {
        for (String parameter : parameters) {
            addParameter(parameter);
        }
    }

    public void addParametersString(@NotNull String parametersString) {
        addParameters(ParametersList.parse(parametersString));
    }

    public List<String> getParameters() {
        return myParamList.getList();
    }

    public ParametersList getParametersList() {
        return myParamList;
    }

    /** @noinspection MethodDoesntCallSuperMethod*/
    @Override
    public ParamsGroup clone() {
        return new ParamsGroup(myGroupId, myParamList.clone());
    }

    @Override
    public String toString() {
        return myGroupId + ":" + myParamList;
    }
}
