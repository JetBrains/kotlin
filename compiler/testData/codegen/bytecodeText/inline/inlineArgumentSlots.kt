// IGNORE_BACKEND: JVM_IR

class Range<T>(val min: T, val max: T)

class Sprite

class A {
    private fun calcSpriteSizeRange(layoutSize: Int,
                                    sprites: Map<Long, Sprite>,
                                    minMargin: Int,
                                    range: Range<Int>)
            : Pair<Range<Int>, Int> {
        val count = sprites.count()
        var spriteBounds = layoutSize / count.toFloat()
        require(spriteBounds * count <= layoutSize) {
            val result = spriteBounds * count <= layoutSize
            "Algorithm incorrect: $spriteBounds * " +
                    "$count == $result <= $layoutSize"
        }
        val adjustedMargin = minMargin + (minMargin / count.toFloat())
        spriteBounds -= adjustedMargin
        var size = spriteBounds
        require((size * count) + minMargin * (count + 1)
                        <= layoutSize) {
            val result = size * count + minMargin * (count + 1)
            "Algorithm incorrect: $size * $count + " +
                    "$minMargin * ($count + 1) == $result <= $layoutSize"
        }
        size = kotlin.math.min(size, range.max.toFloat())
        require(size > 0) {
            "Maximum palantir size ${size.toInt()} > 0."
        }
        val minSize =
            if (range.min > size) {
                size
            } else {
                range.min.toFloat()
            }
        val margin = (layoutSize - (size * count)) / (count + 1)
        val adjustedRange = Range(minSize.toInt(), size.toInt())
        val requiredSize =
            calcRequiredLayoutSize(count, adjustedRange.max, margin.toInt())
        require(requiredSize <= layoutSize) {
            "requiredSize <= layoutSize -> " +
                    "$requiredSize <= $layoutSize"
        }

        return Pair(adjustedRange, margin.toInt())
    }
}

fun calcRequiredLayoutSize(count: Int, max: Int, toInt: Int) = 0

// 3 ISTORE 10