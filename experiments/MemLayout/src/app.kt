package kotlin_native.test

import sun.misc.Unsafe

class Bridge {
    val unsafe = GetUnsafe()

    fun GetUnsafe(): sun.misc.Unsafe {
        val theUnsafe = Unsafe::class.java.getDeclaredField("theUnsafe")
        theUnsafe.isAccessible = true
        return theUnsafe.get(null) as Unsafe
    }

    constructor() {
        System.loadLibrary("bridge-jni")
    }

    fun Lookup(name:String) : NativePtr;

    public native fun Call(NativeFunction func, Array<Any> args)
    public native fun Lookup(String func, String signature) : NativeFunction
}

val bridge = Bridge()

class NativeFunction(name:String, val signature:String) {
    var ptr : Long = bridge.LookupFunction(name)
}

val stat = NativeFunction("stat". "DSP")

class NativePtr(size:Long) {
    var ptr : Long = bridge.unsafe.allocateMemory(size)

    fun IntAt(offset:Int):Int {
        return bridge.unsafe.getInt(ptr+offset)
    }
    fun LongAt(offset:Int):Long {
        return bridge.unsafe.getLong(ptr+offset)
    }
    fun Free() {
        bridge.unsafe.freeMemory(ptr)
        ptr = 0
    }
}

interface Native {
    fun ptr() : NativePtr
}

class StatInfo : Native {
    val ptr = NativePtr(1024)
    val size : Long
      get() =  ptr.LongAt(96)
    val inode : Int
        get() = ptr.IntAt(8)
    override fun ptr() = ptr
}

fun AsCString(s:String):NativePtr {
    val ptr = NativePtr((s.length + 1) as Long)
    return ptr
}

class Layout {
    fun Call(func: String, signature: String, args:Array<Any>) : Any  {

        when (func) {
            "stat" -> {
                val str = AsCString(args[0] as String)
                val args2 = arrayOf(null, str, (args[1] as StatInfo).ptr)
                val rv = bridge.Call(func, signature, args2)
                str.Free()
                return args2[0]
            }
            else -> assert(false)
        }
        return ""
    }
}

fun Cleanup(args:Array<Any>) {
    for (a in args) {
        if (a is Native) {
            a.ptr().Free()
        }
    }
}

fun main(args: Array<String>) {
    val caller = Layout()
    val file = if (args.size > 0) args[0] else "/etc/passwd"
    val args = arrayOf(file, StatInfo())
    val result = caller.Call(bridge.Lookup("stat", "DSP"), args)
    println("got " + (args[1] as StatInfo).size)
    Cleanup(args)
}