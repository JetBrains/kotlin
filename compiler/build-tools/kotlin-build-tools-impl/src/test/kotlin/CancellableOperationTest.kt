import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.internal.CancellableBuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.KotlinToolchainsImpl
import org.jetbrains.kotlin.progress.CompilationCanceledException
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

private class ExampleCancellableOperation : CancellableBuildOperationImpl<Unit>() {
    override fun execute(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
    ) {
        repeat(10) {
            Thread.sleep(100)
            cancellationHandle.checkCanceled()
        }
    }
}


class CancellableOperationTest {
    @OptIn(ExperimentalAtomicApi::class)
    @Test
    fun example() {
        val operation = ExampleCancellableOperation()

        val result = AtomicReference<Unit?>(null)
        val operationWasCancelled = AtomicBoolean(false)

        val thread =
            thread {
                try {
                    result.store(
                        operation.execute(
                            ProjectId.RandomProjectUUID(),
                            KotlinToolchainsImpl().createInProcessExecutionPolicy(),
                            null
                        )
                    )
                } catch (_: CompilationCanceledException) {
                    operationWasCancelled.store(true)
                }
            }
        operation.cancel()
        thread.join()

        assertNull(result.load())
        assertTrue { operationWasCancelled.load() }
    }
}