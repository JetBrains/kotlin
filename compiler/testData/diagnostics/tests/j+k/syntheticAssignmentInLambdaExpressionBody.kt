// FIR_IDENTICAL
// ISSUE: KT-57166
// File order is important.

// FILE: InstanceObject.java
public interface InstanceObject {
    long getDeallocTime();
    void setDeallocTime(long deallocTime);
}

// FILE: LiveAllocationCaptureObject.kt
class LiveAllocationCaptureObject {
    private fun queryJavaInstanceDelta() = run {
        LiveAllocationInstanceObject().deallocTime = 42L
    }
}

// FILE: LiveAllocationInstanceObject.kt
class LiveAllocationInstanceObject: InstanceObject {
    override fun getDeallocTime() = 42L
    override fun setDeallocTime(deallocTime: Long) {}
}
