class Example
{
    var a1 = 0

    public var a2: Int = 0

    private var a3 = 0


    var b1 = 0
        private set

    var b2 = 0
        internal set

    var b3 = 0
        set

    public var b4: Int = 0
        public set

    private var b5 = 0
        private set


    {
        a1 = 1
        a2 = 1
        a3 = 1

        b1 = 1
        b2 = 1
        b3 = 1
        b4 = 1
        b5 = 1
    }
}

// Every property should be accessed directly in this example because they all are final class properties with default accessors
// 0 INVOKESPECIAL Example\.set
// 0 INVOKEVIRTUAL Example\.set
