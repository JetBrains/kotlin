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

package kotlin.jvm.internal;

import kotlin.SinceKotlin;

@SinceKotlin(version = "1.2")
public class MagicApiIntrinsics {
    public static <T> T anyMagicApiCall(int id) {
        return null;
    }

    public static void voidMagicApiCall(int id) {
    }

    public static int intMagicApiCall(int id) {
        return 0;
    }

    public static <T> T anyMagicApiCall(Object data) {
        return null;
    }

    public static void voidMagicApiCall(Object data) {
    }

    public static int intMagicApiCall(Object data) {
        return 0;
    }

    public static int intMagicApiCall(int id, long longData, Object anyData) {
        return 0;
    }

    public static int intMagicApiCall(int id, long longData1, long longData2, Object anyData) {
        return 0;
    }

    public static int intMagicApiCall(int id, Object anyData1, Object anyData2) {
        return 0;
    }

    public static int intMagicApiCall(int id, Object anyData1, Object anyData2, Object anyData3, Object anyData4) {
        return 0;
    }

    public static <T> T anyMagicApiCall(int id, long longData, Object anyData) {
        return null;
    }

    public static <T> T anyMagicApiCall(int id, long longData1, long longData2, Object anyData) {
        return null;
    }

    public static <T> T anyMagicApiCall(int id, Object anyData1, Object anyData2) {
        return null;
    }

    public static <T> T anyMagicApiCall(int id, Object anyData1, Object anyData2, Object anyData3, Object anyData4) {
        return null;
    }
}
