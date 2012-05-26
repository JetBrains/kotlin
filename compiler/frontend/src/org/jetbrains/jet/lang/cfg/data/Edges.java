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

package org.jetbrains.jet.lang.cfg.data;

import org.jetbrains.annotations.NotNull;

/**
* @author svtk
*/
public class Edges<T> {
    public final T in;
    public final T out;

    Edges(@NotNull T in, @NotNull T out) {
        this.in = in;
        this.out = out;
    }

    public static <T> Edges<T> create(@NotNull T in, @NotNull T out) {
        return new Edges<T>(in, out);
    }

    @NotNull
    public T getIn() {
        return in;
    }

    @NotNull
    public T getOut() {
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edges)) return false;

        Edges edges = (Edges) o;

        if (in != null ? !in.equals(edges.in) : edges.in != null) return false;
        if (out != null ? !out.equals(edges.out) : edges.out != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = in != null ? in.hashCode() : 0;
        result = 31 * result + (out != null ? out.hashCode() : 0);
        return result;
    }
}
