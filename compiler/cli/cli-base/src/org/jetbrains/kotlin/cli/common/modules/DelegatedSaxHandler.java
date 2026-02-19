/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.common.modules;

import org.jetbrains.annotations.NotNull;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;

public abstract class DelegatedSaxHandler extends DefaultHandler {

    @NotNull
    protected abstract DefaultHandler getDelegate();

    @Override
    public InputSource resolveEntity(String publicId, @NotNull String systemId) throws IOException, SAXException {
        return getDelegate().resolveEntity(publicId, systemId);
    }

    @Override
    public void notationDecl(@NotNull String name, String publicId, String systemId) throws SAXException {
        getDelegate().notationDecl(name, publicId, systemId);
    }

    @Override
    public void unparsedEntityDecl(@NotNull String name, String publicId, @NotNull String systemId, String notationName)
            throws SAXException {
        getDelegate().unparsedEntityDecl(name, publicId, systemId, notationName);
    }

    @Override
    public void setDocumentLocator(@NotNull Locator locator) {
        getDelegate().setDocumentLocator(locator);
    }

    @Override
    public void startDocument() throws SAXException {
        getDelegate().startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        getDelegate().endDocument();
    }

    @Override
    public void startPrefixMapping(String prefix, @NotNull String uri) throws SAXException {
        getDelegate().startPrefixMapping(prefix, uri);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        getDelegate().endPrefixMapping(prefix);
    }

    @Override
    public void startElement(@NotNull String uri, @NotNull String localName, @NotNull String qName, @NotNull Attributes attributes)
            throws SAXException {
        getDelegate().startElement(uri, localName, qName, attributes);
    }

    @Override
    public void endElement(String uri, @NotNull String localName, @NotNull String qName) throws SAXException {
        getDelegate().endElement(uri, localName, qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        getDelegate().characters(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        getDelegate().ignorableWhitespace(ch, start, length);
    }

    @Override
    public void processingInstruction(@NotNull String target, @NotNull String data) throws SAXException {
        getDelegate().processingInstruction(target, data);
    }

    @Override
    public void skippedEntity(@NotNull String name) throws SAXException {
        getDelegate().skippedEntity(name);
    }

    @Override
    public void warning(@NotNull SAXParseException e) throws SAXException {
        getDelegate().warning(e);
    }

    @Override
    public void error(@NotNull SAXParseException e) throws SAXException {
        getDelegate().error(e);
    }

    @Override
    public void fatalError(@NotNull SAXParseException e) throws SAXException {
        getDelegate().fatalError(e);
    }
}
