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

package quicksort;

public class Quicksort {

    public static void swap(int[] a, int i, int j) {
        int temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }

    public static void quicksort(int[] a, int L, int R) {
        int m = a[(L + R) / 2];
        int i = L;
        int j = R;
        while (i <= j) {
            while (a[i] < m)
                i++;
            while (a[j] > m)
                j--;
            if (i <= j) {
                swap(a, i++, j--);
            }
        }
        if (L < j)
            quicksort(a, L, j);
        if (R > i)
            quicksort(a, i, R);
    }

    public static void quicksort(int[] a) {
        quicksort(a, 0, a.length - 1);
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        // Sample data
        int[] a = new int[100000000];
        for (int i = 0; i < a.length; i++) {
            a[i] = i * 3 / 2 + 1;
            if (i % 3 == 0)
                a[i] = -a[i];
        }

        quicksort(a);

        long total = System.currentTimeMillis() - start;
        System.out.println("[Quicksort-" + System.getProperty("project.name")+ " Benchmark Result: " + total + "]");
    }
}
