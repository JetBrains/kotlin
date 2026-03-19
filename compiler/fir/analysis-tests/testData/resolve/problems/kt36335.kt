// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-36335

// KT-36335: AssertionError: Recursion detected in a lazy value under LockBasedStorageManager
// The issue occurs in K1 frontend when resolving methods from deserialized binary class files
// involving classes with type parameters and inheritance hierarchies.

abstract class AbstractCipher<T> {
    abstract fun processBlock(input: ByteArray, inOff: Int, output: ByteArray, outOff: Int): Int
    abstract fun getAlgorithmName(): String
}

interface CipherParameters

class KeyParameter(val key: ByteArray) : CipherParameters

abstract class BlockCipher<T> : AbstractCipher<T>() {
    abstract fun init(forEncryption: Boolean, params: CipherParameters)
    abstract fun getBlockSize(): Int
}

class AESEngine : BlockCipher<AESEngine>() {
    private var forEncryption = false
    private var key: ByteArray = ByteArray(0)

    override fun init(forEncryption: Boolean, params: CipherParameters) {
        this.forEncryption = forEncryption
        if (params is KeyParameter) {
            key = params.key
        }
    }

    override fun processBlock(input: ByteArray, inOff: Int, output: ByteArray, outOff: Int): Int {
        return getBlockSize()
    }

    override fun getAlgorithmName(): String = "AES"

    override fun getBlockSize(): Int = 16
}

class CCMBlockCipher(private val cipher: BlockCipher<*>) {
    fun processPacket(input: ByteArray, inOff: Int, inLen: Int): ByteArray {
        val output = ByteArray(inLen)
        cipher.processBlock(input, inOff, output, 0)
        return output
    }

    fun getAlgorithmName(): String = "CCM-${cipher.getAlgorithmName()}"

    fun getUnderlyingCipher(): BlockCipher<*> = cipher
}

fun test() {
    val key = KeyParameter(ByteArray(16) { it.toByte() })
    val engine = AESEngine()
    engine.init(true, key)

    val ccm = CCMBlockCipher(engine)
    val input = ByteArray(16)
    val output = ccm.processPacket(input, 0, 16)
    val name = ccm.getAlgorithmName()
    val underlying = ccm.getUnderlyingCipher()
    val blockSize = underlying.getBlockSize()
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, ifExpression, integerLiteral,
interfaceDeclaration, isExpression, lambdaLiteral, localProperty, nullableType, override, primaryConstructor,
propertyDeclaration, smartcast, starProjection, stringLiteral, thisExpression, typeParameter */
