/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package kotlin.internal;

public class Ref {
    private Ref() {}

    public static final class ObjectRef<T> {
        public T element;
    }

    public static final class ByteRef {
        public byte element;
    }

    public static final class ShortRef {
        public short element;
    }

    public static final class IntRef {
        public int element;
    }

    public static final class LongRef {
        public long element;
    }

    public static final class FloatRef {
        public float element;
    }

    public static final class DoubleRef {
        public double element;
    }

    public static final class CharRef {
        public char element;
    }

    public static final class BooleanRef {
        public boolean element;
    }
}
