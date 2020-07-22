// https://ea.jetbrains.com/browser/ea_reports/1337846

//interface ComputablePoint<NumberType : Number>
//
//interface ComputableSegment<NumberType: Number, PointType>
//
//interface ComputableLineSegment<NumberType: Number, PointType> : ComputableSegment<NumberType, PointType>

//interface Path<NumberType, PointType, SegmentType>

typealias EachSegmentComparator<SegmentType> = (currentSegment: SegmentType, otherSegment: SegmentType, relationship: Int) -> Boolean

interface ComputablePath<NumberType, PointType, out SegmentType>
    : <!OTHER_ERROR!>Path<NumberType, PointType, SegmentType><!>
where
    NumberType: Number,
    PointType: <!OTHER_ERROR!>ComputablePoint<NumberType><!>,
    SegmentType: <!OTHER_ERROR!>ComputableLineSegment<NumberType, PointType><!>
{
    fun anyTwoSegments(comparator: EachSegmentComparator<<!OTHER_ERROR!>ComputableSegment<NumberType, PointType><!>>): Boolean
}
