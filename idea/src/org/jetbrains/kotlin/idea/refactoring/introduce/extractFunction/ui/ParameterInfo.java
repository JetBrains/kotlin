/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ui;

import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.Parameter;
import org.jetbrains.kotlin.idea.refactoring.introduce.ui.AbstractParameterTablePanel;
import org.jetbrains.kotlin.types.KotlinType;

public class ParameterInfo extends AbstractParameterTablePanel.AbstractParameterInfo<Parameter> {
    private final boolean receiver;
    private KotlinType type;

    public ParameterInfo(Parameter originalParameter, boolean receiver) {
        super(originalParameter);
        this.receiver = receiver;
        this.type = originalParameter.getParameterType(false);
        setName(receiver ? "<receiver>" : originalParameter.getName());
    }

    public boolean isReceiver() {
        return receiver;
    }

    public KotlinType getType() {
        return type;
    }

    public void setType(KotlinType type) {
        this.type = type;
    }

    @Override
    public Parameter toParameter() {
        return getOriginalParameter().copy(getName(), type);
    }
}
