/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.debugger;

import com.intellij.debugger.NoDataException;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.ClassPrepareRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.NamespaceCodegen;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;

import java.util.*;

/**
 * @author yole
 */
public class JetPositionManager implements PositionManager {
    private final DebugProcess myDebugProcess;
    private WeakHashMap<PsiFile, JetTypeMapper> myTypeMappers = new WeakHashMap<PsiFile, JetTypeMapper>();

    public JetPositionManager(DebugProcess debugProcess) {
        myDebugProcess = debugProcess;
    }

    @Override
    @Nullable
    public SourcePosition getSourcePosition(@Nullable Location location) throws NoDataException {
        if (location == null) {
            throw new NoDataException();
        }
        PsiFile psiFile = getPsiFileByLocation(location);

        int lineNumber;
        try {
            lineNumber = location.lineNumber() - 1;
        } catch (InternalError e) {
            lineNumber = -1;
        }

        if (lineNumber >= 0) {
            return SourcePosition.createFromLine(psiFile, lineNumber);
        }

        throw new NoDataException();
    }

    private PsiFile getPsiFileByLocation(Location location) throws NoDataException {
        try {
            final String sourceName = location.sourceName();
            final Project project = myDebugProcess.getProject();
            final PsiFile[] files = FilenameIndex.getFilesByName(project, sourceName, ProjectScope.getAllScope(project));
            if (files.length == 1 && files[0] instanceof JetFile) {
                return files[0];
            }

        } catch (AbsentInformationException e) {
            throw new NoDataException();
        }


        /* TODO

        final ReferenceType referenceType = location.declaringType();
        if (referenceType == null) {
            return null;
        }

        // TODO
        return null;
        */
        throw new NoDataException();
    }

    @NotNull
    @Override
    public List<ReferenceType> getAllClasses(SourcePosition sourcePosition) throws NoDataException {
        if (!(sourcePosition.getFile() instanceof JetFile)) {
            throw new NoDataException();
        }
        final Collection<String> names = classNamesForPosition(sourcePosition);
        List<ReferenceType> result = new ArrayList<ReferenceType>();
        for (String name : names) {
            result.addAll(myDebugProcess.getVirtualMachineProxy().classesByName(name));
        }
        return result;
    }

    private Collection<String> classNamesForPosition(final SourcePosition sourcePosition) {

        final Collection<String> names = new ArrayList<String>();

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                final JetFile file = (JetFile) sourcePosition.getFile();
                JetTypeMapper typeMapper = prepareTypeMapper(file);

                JetClassOrObject jetClass = PsiTreeUtil.getParentOfType(sourcePosition.getElementAt(), JetClassOrObject.class);
                if (jetClass != null) {
                    names.addAll(typeMapper.allJvmNames(jetClass));
                }
                else {
                    JetFile namespace = PsiTreeUtil.getParentOfType(sourcePosition.getElementAt(), JetFile.class);
                    if (namespace != null) {
                        names.add(NamespaceCodegen.getJVMClassName(JetPsiUtil.getFQName(namespace), true));
                    }
                    else {
                        names.add(NamespaceCodegen.getJVMClassName(JetPsiUtil.getFQName(file), true));
                    }
                }
            }
        });

        return names;
    }

    private JetTypeMapper prepareTypeMapper(JetFile file) {
        final JetTypeMapper mapper = myTypeMappers.get(file);
        if (mapper != null) {
            return mapper;
        }
        final BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(file);
        final JetStandardLibrary standardLibrary = JetStandardLibrary.getJetStandardLibrary(myDebugProcess.getProject());
        final JetTypeMapper typeMapper = new JetTypeMapper(standardLibrary, bindingContext);
        file.acceptChildren(new JetVisitorVoid() {
            @Override
            public void visitJetElement(JetElement element) {
                element.acceptChildren(this);
            }

            @Override
            public void visitClass(JetClass klass) {
                GenerationState.prepareAnonymousClasses(klass, typeMapper);
            }
        });
        myTypeMappers.put(file, typeMapper);
        return typeMapper;
    }

    @NotNull
    @Override
    public List<Location> locationsOfLine(ReferenceType type, SourcePosition position) throws NoDataException {
        if (!(position.getFile() instanceof JetFile)) {
            throw new NoDataException();
        }
        try {
          int line = position.getLine() + 1;
          List<Location> locations = myDebugProcess.getVirtualMachineProxy().versionHigher("1.4")
                                     ? type.locationsOfLine(DebugProcess.JAVA_STRATUM, null, line)
                                     : type.locationsOfLine(line);
          if (locations == null || locations.isEmpty()) throw new NoDataException();
          return locations;
        }
        catch (AbsentInformationException e) {
          throw new NoDataException();
        }
    }

    @Override
    public ClassPrepareRequest createPrepareRequest(ClassPrepareRequestor classPrepareRequestor,
                                                    SourcePosition sourcePosition) throws NoDataException {
        if (!(sourcePosition.getFile() instanceof JetFile)) {
            throw new NoDataException();
        }
        final Collection<String> classNames = classNamesForPosition(sourcePosition);
        if (classNames.isEmpty()) {
            return null;
        }
        final Iterator<String> iterator = classNames.iterator();
        boolean wildcard = false;
        String namePattern = iterator.next();
        while (iterator.hasNext()) {
            namePattern = StringUtil.commonPrefix(namePattern, iterator.next());
            wildcard = true;
        }
        if (wildcard) {
            namePattern += "*";
        }
        return myDebugProcess.getRequestsManager().createClassPrepareRequest(classPrepareRequestor, namePattern.replace('/', '.'));
    }
}
