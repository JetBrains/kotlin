public abstract class VirtualFile() {
    public abstract val size : Long
}

public class PhysicalVirtualFile : VirtualFile() {
    public override val size: Long
    get() = 11
}

fun box() : String {
    PhysicalVirtualFile()
    return "OK"
}
