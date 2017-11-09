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

package kotlin.jvm.internal;

import java.io.Serializable;

public class Ref {
    private Ref() {}

    public static final class ObjectRef<T> implements Serializable {
        public T element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }

    public static final class ByteRef implements Serializable {
        public byte element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }

    public static final class ShortRef implements Serializable {
        public short element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }

    public static final class IntRef implements Serializable {
        public int element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }

    public static final class LongRef implements Serializable {
        public long element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }

    public static final class FloatRef implements Serializable {
        public float element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }

    public static final class DoubleRef implements Serializable {
        public double element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }

    public static final class CharRef implements Serializable {
        public char element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }

    public static final class BooleanRef implements Serializable {
        public boolean element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }
}
