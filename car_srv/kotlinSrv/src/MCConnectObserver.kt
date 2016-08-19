interface MCConnectObserver<in V> {

    fun connect(transportFileName: V)
    fun disconnect()

}