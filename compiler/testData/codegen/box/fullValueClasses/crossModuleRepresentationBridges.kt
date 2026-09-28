// LANGUAGE: +FullValueClasses
// ISSUE: KT-84904

// MODULE: lib
// FILE: lib.kt

package lib

interface Coordinates {
    fun sum(): Int
}

value class Point(val x: Int, val y: Int) : Coordinates {
    override fun sum(): Int = x + y
}

value class Envelope(val point: Point, val id: Long?)

interface Transformer<T> {
    fun transform(value: T): T
}

interface Producer<T> {
    fun produce(): T
}

interface PointOperation {
    fun move(value: Point): Point
}

interface PointFactory {
    fun create(x: Int = 20, y: Int = 21): Point
}

object PointTransformer : Transformer<Point> {
    override fun transform(value: Point): Point =
        Point(value.x + 1, value.y + 1)
}

object NullablePointTransformer : Transformer<Point?> {
    override fun transform(value: Point?): Point? =
        value?.let { Point(it.x + 2, it.y + 2) }
}

object PointProducer : Producer<Point> {
    override fun produce(): Point = Point(30, 31)
}

object NullablePointProducer : Producer<Point?> {
    override fun produce(): Point? = Point(32, 33)
}

object NullPointProducer : Producer<Point?> {
    override fun produce(): Point? = null
}

object MovingPointOperation : PointOperation {
    override fun move(value: Point): Point =
        Point(value.x + 10, value.y + 10)
}

object DefaultPointFactory : PointFactory {
    override fun create(x: Int, y: Int): Point =
        Point(x, y)
}

fun consumeAsInterface(value: Coordinates): Int =
    value.sum()

fun transformThroughGenericBridge(
    transformer: Transformer<Point>,
    value: Point
): Point =
    transformer.transform(value)

fun transformNullableThroughGenericBridge(
    transformer: Transformer<Point?>,
    value: Point?
): Point? =
    transformer.transform(value)

fun produceThroughGenericBridge(
    producer: Producer<Point>
): Point =
    producer.produce()

fun produceNullableThroughGenericBridge(
    producer: Producer<Point?>
): Point? =
    producer.produce()

fun moveThroughPointBridge(
    operation: PointOperation,
    value: Point
): Point =
    operation.move(value)

fun createThroughDefaultDispatch(
    factory: PointFactory
): Point =
    factory.create()

fun makeEnvelope(point: Point, id: Long?): Envelope =
    Envelope(point, id)

fun consumeEnvelope(envelope: Envelope): Long =
    envelope.point.sum().toLong() + (envelope.id ?: -100L)

// MODULE: main(lib)
// FILE: main.kt

import lib.*

fun box(): String {
    val point = Point(1, 2)

    if (consumeAsInterface(point) != 3) {
        return "FAIL interface dispatch"
    }

    val transformed =
        transformThroughGenericBridge(PointTransformer, point)
    if (transformed != Point(2, 3)) {
        return "FAIL generic bridge"
    }

    val transformedNullable =
        transformNullableThroughGenericBridge(
            NullablePointTransformer,
            point
        )
    if (transformedNullable != Point(3, 4)) {
        return "FAIL nullable generic bridge"
    }

    if (
        transformNullableThroughGenericBridge(
            NullablePointTransformer,
            null
        ) != null
    ) {
        return "FAIL nullable generic bridge"
    }

    val produced =
        produceThroughGenericBridge(PointProducer)
    if (produced != Point(30, 31)) {
        return "FAIL generic return bridge"
    }

    val producedNullable =
        produceNullableThroughGenericBridge(NullablePointProducer)
    if (producedNullable != Point(32, 33)) {
        return "FAIL nullable generic return bridge"
    }

    if (produceNullableThroughGenericBridge(NullPointProducer) != null) {
        return "FAIL nullable generic return bridge"
    }

    val moved =
        moveThroughPointBridge(MovingPointOperation, point)
    if (moved != Point(11, 12)) {
        return "FAIL non-generic MFVC bridge"
    }

    val defaultDispatched =
        createThroughDefaultDispatch(DefaultPointFactory)
    if (defaultDispatched != Point(20, 21)) {
        return "FAIL default dispatch"
    }

    val envelope = makeEnvelope(point, 40L)
    if (consumeEnvelope(envelope) != 43L) {
        return "FAIL nested representation"
    }

    val nullableEnvelope = makeEnvelope(point, null)
    if (consumeEnvelope(nullableEnvelope) != -97L) {
        return "FAIL nested nullable representation"
    }

    return "OK"
}
