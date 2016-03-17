public interface PublicInterface {

    companion object {}

    public object ObjPublic
    private object ObjPrivate

    public class NestedPublic
    private  class NestedPrivate
}

internal interface InternalInterface {
    companion object {}
}

internal interface InternalInterfacePrivateCompanion {
    private companion object {}
}

private interface PrivateInterface {
    companion object {}

    public object ObjPublic
    private object ObjPrivate

    public class NestedPublic
    private class NestedPrivate
}

private interface PrivateInterfacePrivateCompanion {
    private companion object
}
