trait Named {
    abstract fun getName() : String;
}

trait MemberDescriptor : Named {}

trait ClassifierDescriptor : Named {}

trait ClassDescriptor : MemberDescriptor, ClassifierDescriptor {}

class ClassDescriptorImpl : ClassDescriptor {
    override fun getName(): String {
        return "OK"
    }
}

class A(val descriptor : ClassDescriptor) {
    val result : String = descriptor.getName()
}

fun box(): String {
    return A(ClassDescriptorImpl()).result
}
