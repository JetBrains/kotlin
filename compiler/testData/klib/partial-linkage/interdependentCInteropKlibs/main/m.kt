import abitestutils.abiTest
import kotlinx.cinterop.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun box() = abiTest {
    expectSuccess(11) { lib2.acceptUnmodifiedStruct(null, 11) }
    expectFailure(linkage("Function 'acceptRemovedStruct' can not be called: Function uses unlinked class symbol 'lib1/RemovedStruct'")) { lib2.acceptRemovedStruct(null, 12) }
    expectSuccess(13) { lib2.acceptStructDuplicatedOnFirstStage(null, 13) }
    expectSuccess(14) { lib2.acceptStructDuplicatedOnSecondStage(null, 14) }
    expectSuccess(15) { lib2.acceptRemovedStructWrapper(null, 15) }
    expectSuccess(16) { allocRemovedStructWrapper(16) }
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private fun allocRemovedStructWrapper(value: Int): Int = memScoped {
    val _ = alloc<lib2.RemovedStructWrapper>()
    return value
}
