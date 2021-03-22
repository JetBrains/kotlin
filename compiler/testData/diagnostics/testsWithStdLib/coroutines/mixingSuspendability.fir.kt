interface AsyncVal { suspend fun getVal(): Int = 1}
interface SyncVal { fun getVal(): Int = 1 }

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class MixSuspend<!> : AsyncVal, SyncVal {

}
