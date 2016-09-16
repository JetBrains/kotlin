CXX = g++
CXXFLAGS = -std=c++11 -g2 -Wall -Werror

all: placer

run: placer
	./placer

clean:
	rm -f *.o placer

placer: placer.o
	$(CXX)  $(CXXFLAGS) -o $@ $<

placer.o: placer.cc
	$(CXX) $(CXXFLAGS) -c $<
