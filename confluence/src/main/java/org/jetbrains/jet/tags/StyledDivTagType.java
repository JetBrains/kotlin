package org.jetbrains.jet.tags;

import org.jetbrains.jet.ConfluenceUtils;

/**
 * @author abreslav
 */

public class StyledDivTagType extends TagType {

    public StyledDivTagType(String tagName) {
        super(tagName);
    }

    @Override
    public void appendOpenTag(StringBuilder builder, TagData tagData) {
        assert tagData.getType() == this;
        builder.append("<div class=\"jet ").append(getTagName()).append("\"");
        if (tagData.getMessage() != null) {
            builder.append(" title=\"");
            ConfluenceUtils.escapeHTML(builder, tagData.getMessage());
            builder.append("\"");
        }
        builder.append(">");
    }

    @Override
    public void appendCloseTag(StringBuilder builder, TagData tagData) {
        builder.append("</div>");
    }
}
