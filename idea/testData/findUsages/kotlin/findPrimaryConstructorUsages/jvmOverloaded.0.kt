// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtPrimaryConstructor
// OPTIONS: usages
public class A @JvmOverloads <caret>constructor (
        public val x: Int = 0,
        public val y: Double = 0.0,
        public val z: String = "0"
)