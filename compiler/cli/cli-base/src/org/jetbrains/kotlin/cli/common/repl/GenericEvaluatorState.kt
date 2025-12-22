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

package org.jetbrains.kotlin.cli.common.repl

import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read

open class GenericReplEvaluatorState(baseClasspath: Iterable<File>, baseClassloader: ClassLoader?, override val lock: ReentrantReadWriteLock = ReentrantReadWriteLock())
    : IReplStageState<EvalClassWithInstanceAndLoader>
{
    override val history: IReplStageHistory<EvalClassWithInstanceAndLoader> = BasicReplStageHistory(lock)

    override val currentGeneration: Int get() = (history as BasicReplStageHistory<*>).currentGeneration.get()

    val topClassLoader: ReplClassLoader = makeReplClassLoader(baseClassloader, baseClasspath)

    val currentClasspath: List<File> get() = lock.read {
        history.peek()?.item?.classLoader?.listAllUrlsAsFiles()
        ?: topClassLoader.listAllUrlsAsFiles()
    }
}

internal fun makeReplClassLoader(baseClassloader: ClassLoader?, baseClasspath: Iterable<File>) =
        ReplClassLoader(URLClassLoader(baseClasspath.map { it.toURI().toURL() }.toTypedArray(), baseClassloader))
