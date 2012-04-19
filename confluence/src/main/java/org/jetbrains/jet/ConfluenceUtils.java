package org.jetbrains.jet;

/**
 * @author Natalia.Ukhorskaya
 */

public class ConfluenceUtils {

    public static void escapeHTML(StringBuilder builder, CharSequence seq) {
        if (seq == null) return;
        for (int i = 0; i < seq.length(); i++) {
            char c = seq.charAt(i);
            switch (c) {
                case '<':
                    builder.append("&lt;");
                    break;
                case '>':
                    builder.append("&gt;");
                    break;
                case '&':
                    builder.append("&amp;");
                    break;
                case ' ':
                    builder.append("&nbsp;");
                    break;
                case '"':
                    builder.append("&quot;");
                    break;
                default:
                    builder.append(c);
            }
        }
    }

    public static String getErrorInHtml(Throwable e, String info) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<div class=\"jet herror\">Jet highlighter error [").append(e.getClass().getSimpleName()).append("]: ");
        ConfluenceUtils.escapeHTML(stringBuilder, e.getMessage());
        stringBuilder.append("<br/>");
        stringBuilder.append("Original text:");
        stringBuilder.append("<pre>");
        ConfluenceUtils.escapeHTML(stringBuilder, info);
        stringBuilder.append("</pre>");
        stringBuilder.append("</div>");
        return stringBuilder.toString();
    }
}
