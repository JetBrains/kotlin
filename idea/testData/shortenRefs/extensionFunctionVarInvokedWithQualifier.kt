object X{ }

<selection>class Y {
    fun f(op: X.()->Unit) {
        X.op()
    }
}</selection>