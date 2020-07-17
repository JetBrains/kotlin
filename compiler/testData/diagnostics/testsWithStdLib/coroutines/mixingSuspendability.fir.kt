interface AsyncVal { suspend fun getVal(): Int = 1}
interface SyncVal { fun getVal(): Int = 1 }

class MixSuspend : AsyncVal, SyncVal {

}
