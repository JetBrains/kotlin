interface Named {
    abstract fun getName() : String;
}

interface MemberDescriptor : Named {}

interface ClassifierDescriptor : Named {}

interface ClassDescriptor : MemberDescriptor, ClassifierDescriptor {}

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
