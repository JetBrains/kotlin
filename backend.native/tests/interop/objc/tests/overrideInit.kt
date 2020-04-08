import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testOverrideInit() {
    assertEquals(42, (TestOverrideInitImpl.createWithValue(42) as TestOverrideInitImpl).value)
}

private class TestOverrideInitImpl @OverrideInit constructor(val value: Int) : TestOverrideInit(value) {
    companion object : TestOverrideInitMeta()
}
