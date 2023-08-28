// FIR_IDENTICAL
// https://ea.jetbrains.com/browser/ea_reports/1337846

//interface ComputablePoint<NumberType : Number>
//
//interface ComputableSegment<NumberType: Number, PointType>
//
//interface ComputableLineSegment<NumberType: Number, PointType> : ComputableSegment<NumberType, PointType>

//interface Path<NumberType, PointType, SegmentType>

typealias EachSegmentComparator<SegmentType> = (currentSegment: SegmentType, otherSegment: SegmentType, relationship: Int) -> Boolean

interface ComputablePath<NumberType, PointType, out SegmentType>
    : <!UNRESOLVED_REFERENCE!>Path<!><NumberType, PointType, SegmentType>
where
    NumberType: Number,
    PointType: <!UNRESOLVED_REFERENCE!>ComputablePoint<!><NumberType>,
    SegmentType: <!UNRESOLVED_REFERENCE!>ComputableLineSegment<!><NumberType, PointType>
{
    fun anyTwoSegments(comparator: EachSegmentComparator<<!UNRESOLVED_REFERENCE!>ComputableSegment<!><NumberType, PointType>>): Boolean
}
