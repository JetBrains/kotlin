public class PublicClass {

    companion object {}

    public object ObjPublic
    internal object ObjInternal
    protected object ObjProtected
    private object ObjPrivate

    public class NestedPublic
    internal class NestedInternal
    protected class NestedProtected
    private  class NestedPrivate

    public inner class InnerPublic
    internal inner class InnerInternal
    protected inner class InnerProtected
    private inner class InnerPrivate
}

internal class InternalClass {
    companion object {}
}

internal class InternalClassInternalCompanion {
    internal companion object {}
}

internal class InternalClassPrivateCompanion {
    private companion object {}
}

private class PrivateClass {
    companion object {}

    public object ObjPublic
    internal object ObjInternal
    protected object ObjProtected
    private object ObjPrivate

    public class NestedPublic
    internal class NestedInternal
    protected class NestedProtected
    private  class NestedPrivate

    public inner class InnerPublic
    internal inner class InnerInternal
    protected inner class InnerProtected
    private inner class InnerPrivate
}

private class PrivateClassInternalCompanion {
    internal companion object
}
private class PrivateClassPrivateCompanion {
    private companion object
}

