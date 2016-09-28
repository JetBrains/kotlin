CXXFLAGS = -std=c++11 -Wall -Werror
CC=$(CXX) #change linker to c++

ifneq ($(OPT), )
CXXFLAGS := $(CXXFLAGS) -O2
else
CXXFLAGS := $(CXXFLAGS) -g2
endif

all: placer

run: placer
	./placer
clean:
	$(RM) *.o placer

placer: placer.o
	$(CXX) $(CXXFLAGS) placer.o -o placer

place.o: placer.cc layout.h
	$(CXX) $(CXXFLAGS) -c placer.cc

.PHONY: all run clean
