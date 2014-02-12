/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package kotlin.jvm.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class SpreadBuilder extends ArrayList<Object> {
    public void addSpread(Object container) {
        if (container == null) return;

        if (container instanceof Object[]) {
            Object[] array = (Object[]) container;
            if (array.length > 0) {
                ensureCapacity(size() + array.length);
                for (Object element : array) {
                    add(element);
                }
            }
        }
        else if (container instanceof Collection) {
            addAll((Collection) container);
        }
        else if (container instanceof Iterable) {
            for (Object element : (Iterable) container) {
                add(element);
            }
        }
        else if (container instanceof Iterator) {
            for (Iterator iterator = (Iterator) container; iterator.hasNext(); ) {
                add(iterator.next());
            }
        }
        else {
            throw new UnsupportedOperationException("Don't know how to spread " + container.getClass());
        }
    }
}
