// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintDrawAllocationInspection
// INSPECTION_CLASS2: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintUseSparseArraysInspection
// INSPECTION_CLASS3: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintUseValueOfInspection

import android.annotation.SuppressLint
import java.util.HashMap
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.SparseArray
import android.widget.Button

@SuppressWarnings("unused")
@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class JavaPerformanceTest(context: Context, attrs: AttributeSet, defStyle: Int) : Button(context, attrs, defStyle) {

    private var cachedRect: Rect? = null
    private var shader: LinearGradient? = null
    private var lastHeight: Int = 0
    private var lastWidth: Int = 0

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)

        // Various allocations:
        java.lang.<warning descr="Avoid object allocations during draw/layout operations (preallocate and reuse instead)">String("foo")</warning>
        val s = java.lang.<warning descr="Avoid object allocations during draw/layout operations (preallocate and reuse instead)">String("bar")</warning>

        // This one should not be reported:
        @SuppressLint("DrawAllocation")
        val i = 5

        // Cached object initialized lazily: should not complain about these
        if (cachedRect == null) {
            cachedRect = Rect(0, 0, 100, 100)
        }
        if (cachedRect == null || cachedRect!!.width() != 50) {
            cachedRect = Rect(0, 0, 50, 100)
        }

        val b = java.lang.Boolean.valueOf(true)!! // auto-boxing
        dummy(1, 2)

        // Non-allocations
        super.animate()
        dummy2(1, 2)

        // This will involve allocations, but we don't track
        // inter-procedural stuff here
        someOtherMethod()
    }

    internal fun dummy(foo: Int?, bar: Int) {
        dummy2(foo!!, bar)
    }

    internal fun dummy2(foo: Int, bar: Int) {
    }

    internal fun someOtherMethod() {
        // Allocations are okay here
        java.lang.String("foo")
        val s = java.lang.String("bar")
        val b = java.lang.Boolean.valueOf(true)!! // auto-boxing


        // Sparse array candidates
        val myMap = <warning descr="Use `new SparseArray<String>(...)` instead for better performance">HashMap<Int, String>()</warning>
        // Should use SparseBooleanArray
        val myBoolMap = <warning descr="Use `new SparseBooleanArray(...)` instead for better performance">HashMap<Int, Boolean>()</warning>
        // Should use SparseIntArray
        val myIntMap = java.util.<warning descr="Use new `SparseIntArray(...)` instead for better performance">HashMap<Int, Int>()</warning>

        // This one should not be reported:
        @SuppressLint("UseSparseArrays")
        val myOtherMap = HashMap<Int, Any>()
    }

    protected fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int,
                            x: Boolean) {
        // wrong signature
        java.lang.String("not an error")
    }

    protected fun onMeasure(widthMeasureSpec: Int) {
        // wrong signature
        java.lang.String("not an error")
    }

    protected fun onLayout(changed: Boolean, left: Int, top: Int, right: Int,
                           bottom: Int, wrong: Int) {
        // wrong signature
        java.lang.String("not an error")
    }

    protected fun onLayout(changed: Boolean, left: Int, top: Int, right: Int) {
        // wrong signature
        java.lang.String("not an error")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int,
                          bottom: Int) {
        java.lang.<warning descr="Avoid object allocations during draw/layout operations (preallocate and reuse instead)">String("flag me")</warning>
    }

    @SuppressWarnings("null") // not real code
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        java.lang.<warning descr="Avoid object allocations during draw/layout operations (preallocate and reuse instead)">String("flag me")</warning>

        // Forbidden factory methods:
        Bitmap.<warning descr="Avoid object allocations during draw/layout operations (preallocate and reuse instead)">createBitmap(100, 100, null)</warning>
        android.graphics.Bitmap.<warning descr="Avoid object allocations during draw/layout operations (preallocate and reuse instead)">createScaledBitmap(null, 100, 100, false)</warning>
        BitmapFactory.<warning descr="Avoid object allocations during draw/layout operations (preallocate and reuse instead)">decodeFile(null)</warning>
        val canvas: Canvas? = null
        canvas!!.<warning descr="Avoid object allocations during draw operations: Use `Canvas.getClipBounds(Rect)` instead of `Canvas.getClipBounds()` which allocates a temporary `Rect`">getClipBounds()</warning> // allocates on your behalf
        canvas.<warning descr="Avoid object allocations during draw operations: Use `Canvas.getClipBounds(Rect)` instead of `Canvas.getClipBounds()` which allocates a temporary `Rect`">clipBounds</warning> // allocates on your behalf
        canvas.getClipBounds(null) // NOT an error

        val layoutWidth = width
        val layoutHeight = height
        if (mAllowCrop && (mOverlay == null || mOverlay!!.width != layoutWidth ||
                           mOverlay!!.height != layoutHeight)) {
            mOverlay = Bitmap.createBitmap(layoutWidth, layoutHeight, Bitmap.Config.ARGB_8888)
            mOverlayCanvas = Canvas(mOverlay!!)
        }

        if (widthMeasureSpec == 42) {
            throw IllegalStateException("Test") // NOT an allocation
        }

        // More lazy init tests
        var initialized = false
        if (!initialized) {
            java.lang.String("foo")
            initialized = true
        }

        // NOT lazy initialization
        if (!initialized || mOverlay == null) {
            java.lang.<warning descr="Avoid object allocations during draw/layout operations (preallocate and reuse instead)">String("foo")</warning>
        }
    }

    internal fun factories() {
        val i1 = 42
        val l1 = 42L
        val b1 = true
        val c1 = 'c'
        val f1 = 1.0f
        val d1 = 1.0

        // The following should not generate errors:
        val i3 = Integer.valueOf(42)
    }

    private val mAllowCrop: Boolean = false
    private var mOverlayCanvas: Canvas? = null
    private var mOverlay: Bitmap? = null

    override fun layout(l: Int, t: Int, r: Int, b: Int) {
        // Using "this." to reference fields
        if (this.shader == null)
            this.shader = LinearGradient(0f, 0f, width.toFloat(), 0f, intArrayOf(0), null,
                                         Shader.TileMode.REPEAT)

        val width = width
        val height = height

        if (shader == null || lastWidth != width || lastHeight != height) {
            lastWidth = width
            lastHeight = height

            shader = LinearGradient(0f, 0f, width.toFloat(), 0f, intArrayOf(0), null, Shader.TileMode.REPEAT)
        }
    }

    fun inefficientSparseArray() {
        <warning descr="Use `new SparseIntArray(...)` instead for better performance">SparseArray<Int>()</warning> // Use SparseIntArray instead
        SparseArray<Long>()    // Use SparseLongArray instead
        <warning descr="Use `new SparseBooleanArray(...)` instead for better performance">SparseArray<Boolean>()</warning> // Use SparseBooleanArray instead
        SparseArray<Any>()  // OK
    }

    fun longSparseArray() {
        // but only minSdkVersion >= 17 or if has v4 support lib
        val myStringMap = HashMap<Long, String>()
    }

    fun byteSparseArray() {
        // bytes easily apply to ints
        val myByteMap = <warning descr="Use `new SparseArray<String>(...)` instead for better performance">HashMap<Byte, String>()</warning>
    }
}
