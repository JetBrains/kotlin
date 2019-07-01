/*
 * Copyright 2000-2017 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package streams.sequence.misc

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf(1, 1, 1, 1, 1, 1, 1, 1).windowed(3, 5).count()
}