package test

private class Impl : InterfaceImpl() {

    private fun kind(kind: Interface.Kind) = getBuilder().setKind(kind)

}
